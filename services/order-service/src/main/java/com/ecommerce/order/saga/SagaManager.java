package com.ecommerce.order.saga;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Saga 模式分散式交易管理器
 * 負責協調多個服務的交易處理和補償
 */
public interface SagaManager<T> {
    
    /**
     * 執行 Saga 交易
     * @param context 交易上下文
     * @return 執行結果
     */
    CompletableFuture<T> execute(SagaContext context);
    
    /**
     * 獲取交易步驟清單
     * @return 交易步驟
     */
    List<SagaStep> getSteps();
    
    /**
     * 新增交易步驟
     * @param step 交易步驟
     */
    void addStep(SagaStep step);
    
    /**
     * 執行補償操作
     * @param context 交易上下文
     * @param failedStepIndex 失敗的步驟索引
     */
    CompletableFuture<Void> compensate(SagaContext context, int failedStepIndex);
}
