package com.example.cloudnotes.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;   // 业务状态码: 200成功, 400失败
    private String message; // 提示信息
    private T data;         // 数据 (泛型: 可以是任何类型)

    // 私有构造，禁止外部直接 new
    private Result() {}

    // ✅ 成功时的快捷方法 (带数据)
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // ✅ 成功时的快捷方法 (不带数据)
    public static <T> Result<T> success() {
        return success(null);
    }

    // ❌ 失败时的快捷方法
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(400); // 这里统一定义业务错误为 400，也可以自定义
        result.setMessage(msg);
        result.setData(null);
        return result;
    }
}