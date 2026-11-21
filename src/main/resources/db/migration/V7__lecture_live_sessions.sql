-- SCRUM-51 part A: some lectures are live sessions given by the teacher at a
-- scheduled time, optionally with a meeting link. Only live lectures that have
-- started can have their attendance taken (SCRUM-28). Existing lectures are
-- self-paced (not live).
alter table lectures add column live boolean default false not null;
alter table lectures add column live_start timestamp(6);
alter table lectures add column meeting_url varchar(500);
