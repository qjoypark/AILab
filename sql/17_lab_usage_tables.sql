-- Lab usage management tables.
-- Target database: lab_management (MySQL).

CREATE TABLE IF NOT EXISTS lab_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'lab room id',
  room_code VARCHAR(50) NOT NULL COMMENT 'lab room code',
  room_name VARCHAR(100) NOT NULL COMMENT 'lab room name',
  building VARCHAR(100) COMMENT 'building',
  floor VARCHAR(50) COMMENT 'floor',
  room_no VARCHAR(50) COMMENT 'room no',
  capacity INT COMMENT 'capacity',
  room_type TINYINT DEFAULT 1 COMMENT 'type:1-teaching,2-research,3-platform,4-other',
  safety_level TINYINT DEFAULT 1 COMMENT 'safety level:1-normal,2-important,3-high-risk',
  equipment_summary TEXT COMMENT 'equipment summary',
  notice TEXT COMMENT 'usage notice',
  status TINYINT DEFAULT 1 COMMENT 'status:0-disabled,1-enabled,2-maintenance',
  created_by BIGINT COMMENT 'created by',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  updated_by BIGINT COMMENT 'updated by',
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  deleted TINYINT DEFAULT 0 COMMENT 'deleted:0-no,1-yes',
  UNIQUE KEY uk_lab_room_code (room_code),
  INDEX idx_lab_room_status (status),
  INDEX idx_lab_room_type (room_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lab room';

CREATE TABLE IF NOT EXISTS lab_room_manager (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
  lab_room_id BIGINT NOT NULL COMMENT 'lab room id',
  manager_id BIGINT NOT NULL COMMENT 'manager user id',
  manager_name VARCHAR(50) NOT NULL COMMENT 'manager name snapshot',
  is_primary TINYINT DEFAULT 0 COMMENT 'primary manager:0-no,1-yes',
  status TINYINT DEFAULT 1 COMMENT 'status:0-disabled,1-enabled',
  created_by BIGINT COMMENT 'created by',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  UNIQUE KEY uk_lab_room_manager (lab_room_id, manager_id),
  INDEX idx_lab_room_manager_room (lab_room_id),
  INDEX idx_lab_room_manager_user (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lab room manager';

CREATE TABLE IF NOT EXISTS lab_usage_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'application id',
  application_no VARCHAR(50) NOT NULL COMMENT 'application no',
  applicant_id BIGINT NOT NULL COMMENT 'applicant id',
  applicant_name VARCHAR(50) NOT NULL COMMENT 'applicant name',
  applicant_dept VARCHAR(100) COMMENT 'applicant department',
  lab_room_id BIGINT NOT NULL COMMENT 'lab room id',
  lab_room_code VARCHAR(50) NOT NULL COMMENT 'lab room code snapshot',
  lab_room_name VARCHAR(100) NOT NULL COMMENT 'lab room name snapshot',
  usage_type TINYINT DEFAULT 1 COMMENT 'usage type:1-teaching,2-research,3-contest,4-training,5-other',
  usage_purpose VARCHAR(500) NOT NULL COMMENT 'usage purpose',
  project_name VARCHAR(200) COMMENT 'course or project name',
  expected_attendee_count INT COMMENT 'expected attendee count',
  start_time DATETIME NOT NULL COMMENT 'start time',
  end_time DATETIME NOT NULL COMMENT 'end time',
  special_equipment TEXT COMMENT 'special equipment requirements',
  safety_commitment TINYINT DEFAULT 0 COMMENT 'safety commitment confirmed:0-no,1-yes',
  status TINYINT DEFAULT 1 COMMENT 'status:1-pending,2-approving,3-approved,4-rejected,5-cancelled',
  approval_status TINYINT DEFAULT 1 COMMENT 'approval status:1-approving,2-approved,3-rejected',
  current_approver_id BIGINT COMMENT 'current approver id',
  current_approver_role VARCHAR(50) COMMENT 'current approver role',
  remark TEXT COMMENT 'remark',
  created_by BIGINT COMMENT 'created by',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  updated_by BIGINT COMMENT 'updated by',
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  deleted TINYINT DEFAULT 0 COMMENT 'deleted:0-no,1-yes',
  UNIQUE KEY uk_lab_usage_application_no (application_no),
  INDEX idx_lab_usage_applicant (applicant_id),
  INDEX idx_lab_usage_room_time (lab_room_id, start_time, end_time),
  INDEX idx_lab_usage_status (status, approval_status),
  INDEX idx_lab_usage_current_approver (current_approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lab usage application';

CREATE TABLE IF NOT EXISTS lab_usage_participant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
  application_id BIGINT NOT NULL COMMENT 'lab usage application id',
  user_id BIGINT NOT NULL COMMENT 'teacher user id',
  real_name VARCHAR(50) NOT NULL COMMENT 'teacher name snapshot',
  dept_name VARCHAR(100) COMMENT 'department name snapshot',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  UNIQUE KEY uk_lab_usage_participant (application_id, user_id),
  INDEX idx_lab_usage_participant_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lab usage participant';
