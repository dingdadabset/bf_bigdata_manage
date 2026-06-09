# Project Documents

This directory keeps design notes, implementation summaries, and planning files out of the project root.

## Structure

- `current-architecture-and-optimization-plan.md`: 当前代码架构设计、风险识别和分阶段迭代优化方案。
- `DGA_Design_Document.md`: overall platform design document.
- `authz-openapi.md`: external authorization API document and curl test cases.
- `database-change-log.md`: manual database migration notes and table/index change records.
- `data-map/`: data map feature implementation notes and summaries.
- `planning/todo.md`: project roadmap and task notes.
- `plans/starrocks-role-template-requirements.md`: StarRocks 角色模板化治理需求梳理。





mysql -h10.0.20.108 -ucs_hadoop -pcs_hadoop@6407 azkaban -e "
select project_id,version,upload_time,uploader
from project_versions
where project_id=1877
order by version desc
limit 5;
"



find projects/6.version -maxdepth 2 -type f | head -100
