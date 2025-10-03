package shop.zailushang.starter;

import shop.zailushang.component.Reader;
import shop.zailushang.flow.FlowEngine;

import java.util.List;

/**
 * 本程序在编时，该网站大多数书籍均为免费阅读，后该网站调整运营策略，全文章需要 VIP校验，仅前 20 章可免费阅读
 * 最新流程：    Reader[载]  →  Selector[择]  →  Parser[析]  →  Decoder[译]  →  Formatter[椠]  →  Writer[录]  →  Merger[撰]  →  Cleaner[涤]
 * 下载VIP章节需在 {@link Reader#read0 } 中添加对应 cookie 认证
 * 默认写入文件路径 {@link FlowEngine#FOLDER_FORMATTER } 可修改默认位置，修改前缀即可
 * 下载完成时默认清理零散章节 {@link FlowEngine#NEED_DELETE } 可控制关闭
 * 默认开启测试模式 {@link FlowEngine#IS_TEST } 可控制关闭，测试模式下仅下载前 20 章内容
 * 每个线程负责处理的资源数量 {@link FlowEngine#DEFAULT_CAPACITY } 默认合并 100 章
 */
public class ShuQiSpiderStarter {
    public static void main(String[] args) {
        // 禁用 GraalVM 警告日志
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        try (var engine = FlowEngine.getDefaultFlowEngine()) {
            // 单独下载一本
            //engine.start("武动乾坤");
            // 多本一起下
            List.of("斗破苍穹", "武动乾坤", "大主宰", "元尊").parallelStream().forEach(engine::start);
        }
    }
}