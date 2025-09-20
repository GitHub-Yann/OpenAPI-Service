package org.openapi.common;

public class BaseResponse<T> {
	private boolean result = true;
	private String errorMsg;
	private Integer errorCode;
	private T data;

	public BaseResponse(boolean result, String errorMsg, T obj) {
		this.result = result;
		this.errorMsg = errorMsg;
		this.data = obj;
	}

	public BaseResponse(boolean result, Integer errorCode, String errorMsg, T obj) {
		this.result = result;
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
		this.data = obj;
	}

	public BaseResponse() {
	}

	public static <T> BaseResponse<T> OK(T obj) {
		return new BaseResponse<>(true, "Api access succeeded", obj);
	}

	public static <T> BaseResponse<T> OK(String msg, T obj) {
		return new BaseResponse<>(true, msg, obj);
	}

	public static <T> BaseResponse<T> ERROR(String errorMsg) {
		return new BaseResponse<>(false, 500,errorMsg, null);
	}

	public static <T> BaseResponse<T> ERROR(Integer errorCode, String errorMsg) {
		return new BaseResponse<>(false, errorCode, errorMsg, null);
	}

	public static String toErrorJsonString(Integer errorCode, String errorMsg){
		return "{\"result\":false,\"errorCode\":"+errorCode+",\"errorMsg\":\""+errorMsg+"\",\"data\":null}";
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}
}
