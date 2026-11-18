-- SCRUM-45: some courses require attendance. Existing courses do not.
-- The attendance tracker itself comes with SCRUM-28.
alter table courses add column attendance_required boolean default false not null;
