IF OBJECT_ID(N'dbo.sys_department', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sys_department
    (
        id          BIGINT IDENTITY (1,1) NOT NULL PRIMARY KEY,
        dept_code   NVARCHAR(64)  NOT NULL,
        dept_name   NVARCHAR(128) NOT NULL,
        parent_id   BIGINT NULL,
        leader_id   BIGINT NULL,
        sort        INT NULL DEFAULT 0,
        status      INT NULL DEFAULT 1,
        deleted     INT NULL DEFAULT 0,
        create_time DATETIME2 NULL,
        update_time DATETIME2 NULL
    );
    CREATE UNIQUE INDEX ux_sys_department_code ON dbo.sys_department (dept_code);
    CREATE INDEX ix_sys_department_parent ON dbo.sys_department (parent_id);
    CREATE INDEX ix_sys_department_leader ON dbo.sys_department (leader_id);
END;

IF OBJECT_ID(N'dbo.sys_position', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sys_position
    (
        id             BIGINT IDENTITY (1,1) NOT NULL PRIMARY KEY,
        position_code  NVARCHAR(64)  NOT NULL,
        position_name  NVARCHAR(128) NOT NULL,
        position_level INT NULL DEFAULT 99,
        sort           INT NULL DEFAULT 0,
        status         INT NULL DEFAULT 1,
        deleted        INT NULL DEFAULT 0,
        create_time    DATETIME2 NULL,
        update_time    DATETIME2 NULL
    );
    CREATE UNIQUE INDEX ux_sys_position_code ON dbo.sys_position (position_code);
END;

IF OBJECT_ID(N'dbo.sys_user', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sys_user
    (
        id          BIGINT IDENTITY (1,1) NOT NULL PRIMARY KEY,
        username    NVARCHAR(64)  NOT NULL,
        password    NVARCHAR(128) NOT NULL,
        real_name   NVARCHAR(64)  NULL,
        role        NVARCHAR(32)  NULL DEFAULT N'employee',
        dept_id     BIGINT NULL,
        position_id BIGINT NULL,
        email       NVARCHAR(128) NULL,
        phone       NVARCHAR(32)  NULL,
        status      INT NULL DEFAULT 1,
        deleted     INT NULL DEFAULT 0,
        create_time DATETIME2 NULL,
        update_time DATETIME2 NULL
    );
    CREATE UNIQUE INDEX ux_sys_user_username ON dbo.sys_user (username);
    CREATE INDEX ix_sys_user_dept ON dbo.sys_user (dept_id);
    CREATE INDEX ix_sys_user_position ON dbo.sys_user (position_id);
END;

IF OBJECT_ID(N'dbo.leave_request', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.leave_request
    (
        id                  BIGINT IDENTITY (1,1) NOT NULL PRIMARY KEY,
        process_instance_id NVARCHAR(64) NULL,
        apply_user_id       BIGINT NULL,
        apply_user_name     NVARCHAR(64) NULL,
        dept_id             BIGINT NULL,
        dept_name           NVARCHAR(128) NULL,
        position_id         BIGINT NULL,
        position_name       NVARCHAR(128) NULL,
        dept_leader_id      BIGINT NULL,
        dept_leader_name    NVARCHAR(64) NULL,
        upper_leader_id     BIGINT NULL,
        upper_leader_name   NVARCHAR(64) NULL,
        days                INT NULL,
        start_date          DATE NULL,
        end_date            DATE NULL,
        reason              NVARCHAR(500) NULL,
        status              INT NULL DEFAULT 0,
        deleted             INT NULL DEFAULT 0,
        create_time         DATETIME2 NULL,
        update_time         DATETIME2 NULL
    );
    CREATE INDEX ix_leave_request_process ON dbo.leave_request (process_instance_id);
    CREATE INDEX ix_leave_request_user ON dbo.leave_request (apply_user_id);
    CREATE INDEX ix_leave_request_dept ON dbo.leave_request (dept_id);
    CREATE INDEX ix_leave_request_dept_leader ON dbo.leave_request (dept_leader_id);
    CREATE INDEX ix_leave_request_upper_leader ON dbo.leave_request (upper_leader_id);
END;

IF OBJECT_ID(N'dbo.wf_form', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_form
    (
        id          BIGINT IDENTITY (1,1) NOT NULL PRIMARY KEY,
        form_key    NVARCHAR(64)  NOT NULL,
        name        NVARCHAR(128) NOT NULL,
        category    NVARCHAR(64)  NULL,
        schema_json NVARCHAR(MAX) NULL,
        remark      NVARCHAR(500) NULL,
        status      INT NULL DEFAULT 1,
        deleted     INT NULL DEFAULT 0,
        create_time DATETIME2 NULL,
        update_time DATETIME2 NULL
    );
    CREATE UNIQUE INDEX ux_wf_form_key ON dbo.wf_form (form_key);
END;

IF OBJECT_ID(N'dbo.sys_file', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sys_file
    (
        id            BIGINT IDENTITY (1,1) NOT NULL PRIMARY KEY,
        original_name NVARCHAR(255) NOT NULL,
        stored_name   NVARCHAR(255) NOT NULL,
        content_type  NVARCHAR(128) NULL,
        file_size     BIGINT NULL,
        storage_path  NVARCHAR(500) NOT NULL,
        uploader_id   BIGINT NULL,
        uploader_name NVARCHAR(64) NULL,
        create_time   DATETIME2 NULL
    );
    CREATE UNIQUE INDEX ux_sys_file_stored_name ON dbo.sys_file (stored_name);
END;
