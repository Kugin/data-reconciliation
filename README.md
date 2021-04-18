# data-reconciliation

## 项目简介
通用对账处理工具
- 实现目的：大批量的比较任意两个数据源的任意指定数据内容间的差异，并能自动进行自定义的数据修改同步操作。
> 当前版本：`0.0.1-SNAPSHOT`

## 使用说明
项目执行器`CheckExecutor`是由三个处理器连接起来的：
- `BeforeCheckProcessor`：前置处理器，负责准备对账的双方数据，以及一些前置必要的校验等逻辑。
- `CheckProcessor`：对账处理器，负责双方数据源指定对比内容的数据对比。
- `AfterCheckProcessor`：后置处理器，负责对对账结果的处理，包含自动调账，自定义操作等逻辑。

![CheckExecutor](http://data.kugin.top/other%2FCheckExecutor.png)

对比实体`CheckEntry`的转化方式现实现两种：
1. 基于注解`@CheckIdentity`比较唯一标识,`@CheckField`待比较的字段。
2. 基于适配接口`CheckAdapter`，不同的对比实体实现该接口的`getKey()`与`getCheckData()`方法即可。


`CheckConfig`的配置参数说明：

|         参数         |        参数描述        | 是否必须 |                           备注                           |
| :------------------: | :--------------------: | :------: | :------------------------------------------------------: |
|          id          |      对账唯一标识      |    C     |        name与id不可同时为空,id为空时默认使用name         |
|         name         |        对账名称        |    N     |            名称可能重复，name与id不可同时为空            |
|       checkPre       |        前置校验        |    N     |                支持函数式与自定义接口实现                |
|      srcLoader       |       上游数据源       |    C     |                支持函数式与自定义接口实现                |
|     targetLoader     |       下游数据源       |    C     |                支持函数式与自定义接口实现                |
|    resourceReader    | 全数据源（包含上下游） |    C     | 与`srcLoader`、`targetLoade`r 二选一，支持自定义接口实现 |
|      checkSync       |    对账调账同步处理    |    N     |                  默认不做修改，直接通过                  |
|      checkAfter      |    对账后置处理逻辑    |    N     |                     默认不做任何处理                     |
| beforeCheckProcessor |       前置处理器       |    N     |    可以自定义实现或继承`AbstractBeforeCheckProcessor`    |
|    checkProcessor    |       对比处理器       |    N     |       可以自定义实现或继承`AbstractCheckProcessor`       |
| afterCheckProcessor  |       后置处理器       |    N     |    可以自定义实现或继承`AbstractAfterCheckProcessor`     |


使用样例参考 `CheckEntryTest`：对于对比实体的包装方式，`ExecutorTest`：执行器的构建与执行。


### TODO:
- [ ] 基于redis的对账处理器实现
- [ ] 基于redis的执行器管理
- [ ] 资源加载器FileResourceLoader实现自动解析
- [ ] 相同形式的数据源对比,流式处理比对