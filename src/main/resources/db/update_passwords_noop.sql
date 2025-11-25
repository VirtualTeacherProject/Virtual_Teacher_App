-- Backup first if possible (export users table)
UPDATE users
SET password = '{noop}' || password
WHERE password NOT LIKE '{bcrypt}%'
  AND password NOT LIKE '{noop}%';