package com.flashflow.common.context;

/**
 * 请求级用户上下文（ThreadLocal，由 UserContextInterceptor 在每次请求时设置）。
 *
 * Gateway 通过 AuthGlobalFilter 校验 JWT 后注入 X-User-Id / X-User-Name / X-User-Role 头，
 * 下游服务通过此 Holder 获取当前用户身份，消除 @RequestParam userId 的 IDOR 风险。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_NAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    private UserContext() {}

    public static void set(Long userId, String userName, String role) {
        USER_ID.set(userId);
        USER_NAME.set(userName);
        ROLE.set(role);
    }

    public static void clear() {
        USER_ID.remove();
        USER_NAME.remove();
        ROLE.remove();
    }

    /** 当前登录用户 ID */
    public static Long getUserId() { return USER_ID.get(); }

    /** 当前登录用户名 */
    public static String getUserName() { return USER_NAME.get(); }

    /** 当前登录角色 */
    public static String getRole() { return ROLE.get(); }

    /** 是否是管理员 */
    public static boolean isAdmin() { return "ROLE_ADMIN".equals(ROLE.get()); }
}
