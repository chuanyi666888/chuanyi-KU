-- 修复 admin/keeper01/auditor01 密码哈希为 Admin@123456 的正确 BCrypt 值
UPDATE sys_user SET password = '$2b$10$jkDPeDRSstuxWzcjTPweA.zQJz5r7/2ONzvb3ERTEgWt1G4A77FWe' WHERE id IN (1, 2, 3);
SELECT id, username, LEFT(password, 20) AS pwd_prefix, LENGTH(password) AS pwd_len FROM sys_user;
