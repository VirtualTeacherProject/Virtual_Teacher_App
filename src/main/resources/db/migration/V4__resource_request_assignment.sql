-- SCRUM-36: a teacher assigns a resource request to another enrolled student,
-- who answers it with text (links and notes). Statuses are OPEN, IN_PROGRESS,
-- RESOLVED and IRRELEVANT; status is already varchar(20), so it is unchanged.
alter table resource_requests add column assignee_id bigint;
alter table resource_requests add column response varchar(2000);
alter table resource_requests add column responded_at timestamp(6);
alter table resource_requests add constraint fk_resource_requests_assignee
    foreign key (assignee_id) references users (user_id);
