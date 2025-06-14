package com.ecommerce.order.saga;

import java.util.concurrent.CompletableFuture;

/**
 * Saga 交易步驟介面
 * 定義每個交易步驟的執行和補償邏輯
 */
public interface SagaStep {
    
    /**
     * 獲取步驟名稱
     */
    String getStepName();
    
    /**
     * 執行步驟
     * @param context 交易上下文
     * @return 執行結果
     */
    CompletableFuture<Void> execute(SagaContext context);
    
    /**
     * 補償操作
     * @param context 交易上下文
     * @return 補償結果
     */
    CompletableFuture<Void> compensate(SagaContext context);
    
    /**
     * 是否需要補償
     * @param context 交易上下文
     * @return 如需補償則返回 true
     */
    default boolean needsCompensation(SagaContext context) {
        return true;
    }
    
    /**
     * 獲取步驟執行順序
     * @return 執行順序（數字越小越先執行）
     */
    default int getOrder() {
        return 0;
    }
}
