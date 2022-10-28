CREATE OR REPLACE FUNCTION get_user_groups(userid character varying(13))
  RETURNS TABLE (
   groupid character varying(13)
  ) AS
$func$
BEGIN

   RETURN QUERY
   WITH RECURSIVE subgroups (kid, name, parentgroup, path, depth) AS (
    SELECT ug.kid, ug.name, uga.parentgroup, ARRAY[uga.parentgroup], 1
    FROM obj_010 ug
    INNER JOIN obj_011 uga ON uga.childgroup = ug.kid
  UNION ALL
	
    SELECT ug1.kid, ug1.name, uga1.parentgroup, array_append(path, uga1.parentgroup)::character varying(13)[], si.depth + 1
    FROM obj_010 ug1
    INNER JOIN obj_011 uga1 ON uga1.childgroup = ug1.kid
    INNER JOIN subgroups si ON si.kid = uga1.parentgroup
  )

SELECT path[array_length(path, 1)] as groupid
FROM subgroups
where kid in (select parentgroup from obj_011 where childuser = userid)
UNION ALL
SELECT parentgroup as groupid
from obj_011 where childuser = userid;

END
$func$  LANGUAGE plpgsql;