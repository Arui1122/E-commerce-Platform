package com.ecommerce.order.saga.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.ecommerce.order.saga.SagaContext;
import com.ecommerce.order.saga.SagaManager;
import com.ecommerce.order.saga.SagaStep;

import lombok.extern.slf4j.Slf4j;

/**
 * Saga 交易管理器實現
 * 實現分散式交易的編排和補償邏輯
 */
@Component
@Slf4j
public class DefaultSagaManager implements SagaManager<Void> {
    
    private final List<SagaStep> steps = new ArrayList<>();
    
    @Override
    public CompletableFuture<Void> execute(SagaContext context) {
        log.info("開始執行 Saga 交易: {}", context.getTransactionId());
        context.setStatus(SagaContext.SagaStatus.EXECUTING);
        
        // 根據順序排序步驟
        List<SagaStep> sortedSteps = steps.stream()
                .sorted(Comparator.comparingInt(SagaStep::getOrder))
                .toList();
        
        return executeSteps(context, sortedSteps, 0);
    }
    
    /**
     * 遞迴執行步驟
     */
    private CompletableFuture<Void> executeSteps(SagaContext context, List<SagaStep> steps, int currentIndex) {
        if (currentIndex >= steps.size()) {
            // 所有步驟執行完成
            context.setStatus(SagaContext.SagaStatus.COMPLETED);
            log.info("Saga 交易執行完成: {}", context.getTransactionId());
            return CompletableFuture.completedFuture(null);
        }
        
        SagaStep currentStep = steps.get(currentIndex);
        log.info("執行步驟 {}: {}", currentIndex + 1, currentStep.getStepName());
        
        return currentStep.execute(context)
                .thenCompose(result -> {
                    // 當前步驟成功，執行下一步
                    return executeSteps(context, steps, currentIndex + 1);
                })
                .exceptionally(throwable -> {
                    // 當前步驟失敗，開始補償
                    log.error("步驟 {} 執行失敗: {}", currentStep.getStepName(), throwable.getMessage());
                    context.setStatus(SagaContext.SagaStatus.COMPENSATING);
                    
                    // 執行補償操作
                    compensate(context, currentIndex).join();
                    
                    context.setStatus(SagaContext.SagaStatus.FAILED);
                    throw new RuntimeException("Saga 交易執行失敗", throwable);
                });
    }
    
    @Override
    public List<SagaStep> getSteps() {
        return new ArrayList<>(steps);
    }
    
    @Override
    public void addStep(SagaStep step) {
        this.steps.add(step);
        log.info("新增 Saga 步驟: {}", step.getStepName());
    }
    
    @Override
    public CompletableFuture<Void> compensate(SagaContext context, int failedStepIndex) {
        log.info("開始執行補償操作，失敗步驟索引: {}", failedStepIndex);
        
        // 反向執行補償操作（從失敗步驟的前一步開始）
        List<CompletableFuture<Void>> compensationFutures = new ArrayList<>();
        
        for (int i = failedStepIndex - 1; i >= 0; i--) {
            SagaStep step = steps.get(i);
            if (step.needsCompensation(context)) {
                log.info("執行補償: {}", step.getStepName());
                CompletableFuture<Void> compensationFuture = step.compensate(context)
                        .exceptionally(throwable -> {
                            log.error("補償操作失敗: {}, 錯誤: {}", 
                                    step.getStepName(), throwable.getMessage());
                            return null;
                        });
                compensationFutures.add(compensationFuture);
            }
        }
        
        return CompletableFuture.allOf(compensationFutures.toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                    context.setStatus(SagaContext.SagaStatus.COMPENSATED);
                    log.info("補償操作完成: {}", context.getTransactionId());
                });
    }
}
