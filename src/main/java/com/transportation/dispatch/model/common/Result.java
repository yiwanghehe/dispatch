package com.transportation.dispatch.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok 注解，自动生成 getter, setter, equals, hashCode, toString 方法
@AllArgsConstructor // Lombok 注解，自动生成包含所有字段的构造函数
@NoArgsConstructor // Lombok 注解，自动生成无参构造函数
public class Result {
    private int code; // 对应 result.getCode()
    private String msg; // 对应 result.getMsg()
    private Object data; // 对应 result.getData()

    // 静态工厂方法，方便创建 Result 实例 (与您代码中 Result.success 和 Result.error 调用对应)
    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    public static Result success(String msg) {
        return new Result(200, msg, null);
    }

    public static Result success(String msg, Object data) {
        return new Result(200, msg, data);
    }

    public static Result success() {
        return new Result(200, "success", null);
    }

    public static Result error(String msg) {
        return new Result(500, msg, null);
    }

    // 手动 Getter 和 Setter 方法 (如果您没有使用 Lombok，则必须手动添加这些方法)
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
