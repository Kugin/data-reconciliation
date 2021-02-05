# data-reconciliation
通用对账处理器
> 当前版本：`0.0.1-SNAPSHOT`

完成基本功能:
1. 对比实体组装两种形式:基于注解和基于接口的适配模式
2. 对账流程分为3个步骤processor及默认实现
2. 入口执行器CheckExecutor,简单生命周期管理
3. 防重复执行

TODO:
- [ ] 基于redis的对账处理器实现
- [ ] 基于redis的执行器管理
- [ ] 资源加载器FileResourceLoader实现自动解析
- [ ] 相同形式的数据源对比,流式处理比对
