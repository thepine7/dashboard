package com.andrew.hnt.api.exception;

/**
 * 리소스 없음 예외
 * 요청한 리소스를 찾을 수 없을 때 발생
 */
public class ResourceNotFoundException extends BusinessException {
    
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", 
            String.format("%s를 찾을 수 없습니다. ID: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId, String message) {
        super("RESOURCE_NOT_FOUND", message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
}

