create table if not exists PERSON (
  ID int identity primary key,
  NAME varchar,
  LAST_NAME varchar,
  BORN  DATETIME,
  ORG varchar(30)
)
