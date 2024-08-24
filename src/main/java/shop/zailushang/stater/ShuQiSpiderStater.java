package shop.zailushang.stater;

import shop.zailushang.flow.FlowEngine;

import java.util.List;

/**
 * 只能用于下载免费阅读的章节，VIP章节虽然走的是同样的接口，但肯定是会对资源做了登录权限校验的
 * 下载VIP内容时，接口返回为空，对应在代码中则是：在使用 Decoder 进行解密时会出错，比如：大主宰，只能下载前20章免费章节
 * 要下载VIP章节，你得有VIP账号，至于鉴权，要么是cookie，要么其他特殊请求头
 * 调试一下，在 {@link shop.zailushang.component.Reader#read0 } 中添加对应请求头即可，本人没有VIP账号就不试了
 */
public class ShuQiSpiderStater {
    public static void main(String[] args) {
        // 禁用 GraalVM 警告日志
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        try (var engine = FlowEngine.getDefaultFlowEngine()) {
            // 单独下载一本
            engine.start("斗破苍穹");
            // 多本一起下
            // List.of("斗破苍穹", "武动乾坤", "大主宰", "元尊").forEach(engine::start);
        }
    }
}