## A simple Java web spider for downloading e-books from ShuQi Novels

https://blog.csdn.net/qq_40553917/article/details/140622178

### 内容概览

- 功能：Java爬虫
- 来源：书旗小说网免费章节
- 环境：JDK25
- 涉及：
	1. 流程设计
	2. 设计模式
	3. 任务编排
	4. 并发控制
	5. 多线程文件合并
	6. 分治思想
	7. 池化思想

---



### 流程概览

```mermaid
---
title: Flow 调用流程图
---
flowchart TD
    Reader([Reader：执行请求下载])
    --> Selector([Selector：执行元素选择])
    --> Parser([Parser：执行文本解析])
    -.-> Decoder([Decoder：执行文本解密])
    -.-> Formatter([Formatter：执行文本格式化])
    -.-> Writer([Writer：执行文件写入])
    -.-> Merger([Merger：执行文件合并])
    -.-> Cleaner([Cleaner：执行零散章节文件删除])
```

```mermaid
sequenceDiagram
	title Flow调用时序图
    participant Flow
    participant Reader
    participant Selector
    participant Parser
	participant Decoder
	participant Formatter
	participant Writer
	participant Merger
	participant Cleaner

    Flow->>+Reader: read()
    Reader-->>-Flow: 返回响应文本
    
    Flow->>+Selector: select()
    Selector-->>-Flow: 返回选择的页面元素
    
    Flow->>+Parser: parse()
    Parser-->>-Flow: 返回解析后的文本
    
    Flow->>+Decoder: decode()
    Decoder-->>-Flow: 返回解密后的文本
    
    Flow->>+Formatter: format()
    Formatter-->>-Flow: 返回格式化后的文本
    
    Flow->>+Writer: write()
    Writer-->>-Flow: 返回写入完成的文件列表
    
    Flow->>+Merger: merge()
    Merger-->>-Flow: 返回合并完成的文件列表
    
    Flow->>+Cleaner: clean()
    Cleaner-->>-Flow: 返回为空
```

```mermaid
---
title: Merger(文件合并器)运行原理(IOForkJoinTask)
---
flowchart TD
	taskStart((开始))
    compute([task#compute])
    needFork{task#needFork}
    fork([task#fork])
    noFork([task#doCompute])
    join([task#join])
    task1([执行任务拆分：task1])
    task2([执行任务拆分：task2])
    taskEnd((结束))

    taskStart --提交异步任务--> compute
    compute --执行判断--> needFork
    needFork --是--> fork
    needFork --否--> noFork
    noFork --计算任务结果直接返回--> taskEnd
    fork --> task1
    fork --> task2
    task1 --提交子任务，并阻塞等待子任务返回--> join
    task2 --提交子任务，并阻塞等待子任务返回--> join
    join -.递归.-> taskStart
    join --合并子任务结果--> taskEnd
```
