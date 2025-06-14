package com.ecommerce.order.saga;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * Saga 交易上下文
 * 保存交易執行過程中的資料和狀態
 */
@Data
public class SagaContext {
    
    /**
     * 交易ID
     */
    private String transactionId;
    
    /**
     * 使用者ID
     */
    private Long userId;
    
    /**
     * 訂單ID
     */
    private Long orderId;
    
    /**
     * 交易狀態
     */
    private SagaStatus status;
    
    /**
     * 上下文資料
     */
    private Map<String, Object> data;
    
    /**
     * 補償資料
     */
    private Map<String, Object> compensationData;
    
    public SagaContext() {
        this.data = new HashMap<>();
        this.compensationData = new HashMap<>();
        this.status = SagaStatus.PENDING;
    }
    
    public SagaContext(String transactionId, Long userId) {
        this();
        this.transactionId = transactionId;
        this.userId = userId;
    }
    
    /**
     * 新增資料到上下文
     */
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
    
    /**
     * 從上下文獲取資料
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) this.data.get(key);
    }
    
    /**
     * 新增補償資料
     */
    public void addCompensationData(String key, Object value) {
        this.compensationData.put(key, value);
    }
    
    /**
     * 獲取補償資料
     */
    @SuppressWarnings("unchecked")
    public <T> T getCompensationData(String key) {
        return (T) this.compensationData.get(key);
    }
    
    /**
     * Saga 交易狀態列舉
     */
    public enum SagaStatus {
        PENDING,     // 等待執行
        EXECUTING,   // 執行中
        COMPLETED,   // 完成
        COMPENSATING, // 補償中
        COMPENSATED,  // 已補償
        FAILED       // 失敗
    }
}
