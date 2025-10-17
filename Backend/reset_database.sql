-- Script để xóa và tạo lại database
USE master;
GO

-- Ngắt kết nối tất cả user khỏi database
ALTER DATABASE EV_Management SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
GO

-- Xóa database
DROP DATABASE IF EXISTS EV_Management;
GO

-- Tạo lại database
CREATE DATABASE EV_Management;
GO

-- Chuyển về multi-user
ALTER DATABASE EV_Management SET MULTI_USER;
GO

PRINT 'Database EV_Management đã được reset thành công!';
