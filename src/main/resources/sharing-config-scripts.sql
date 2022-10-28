CREATE TABLE userrecordsharing
(
  recordid character varying(13),
  assigneduser character varying(13),
  CONSTRAINT userrecordsharing_recordid_assigneduser_key UNIQUE (recordid, assigneduser)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE userrecordsharing
  OWNER TO kolmuenv;
  
-- Function: insert_sharing()

-- DROP FUNCTION insert_sharing();

CREATE OR REPLACE FUNCTION insert_sharing()
  RETURNS trigger AS
$BODY$

BEGIN

PERFORM * FROM userrecordsharing WHERE recordid = NEW.recordid AND assigneduser = NEW.assigneduser;
IF NOT FOUND THEN
	insert into userrecordsharing (recordid, assigneduser) values (NEW.recordid, NEW.assigneduser);
END IF;

RETURN NULL;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION insert_sharing()
  OWNER TO kolmuenv;


-- Function: delete_sharing()

-- DROP FUNCTION delete_sharing();

CREATE OR REPLACE FUNCTION delete_sharing()
  RETURNS trigger AS
$BODY$

BEGIN

PERFORM * FROM obj_00o WHERE recordid = OLD.recordid AND assigneduser = OLD.assigneduser;
IF NOT FOUND THEN
	delete from userrecordsharing where recordid = OLD.recordid and assigneduser = OLD.assigneduser;
END IF;

RETURN NULL;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION delete_sharing()
  OWNER TO kolmuenv;


CREATE TRIGGER delete_sharing_trigger
  AFTER DELETE
  ON obj_00o
  FOR EACH ROW
  EXECUTE PROCEDURE delete_sharing();
  
-- Trigger: insert_sharing_trigger on obj_00o

-- DROP TRIGGER insert_sharing_trigger ON obj_00o;

CREATE TRIGGER insert_sharing_trigger
  AFTER INSERT
  ON obj_00o
  FOR EACH ROW
  EXECUTE PROCEDURE insert_sharing();

