-- Some managed Windows PostgreSQL installations prohibit loading unaccent.dll through
-- application-control policy. Keep the search SQL portable by shadowing the extension function
-- in the application schema. PostgreSQL resolves this function before public.unaccent(text)
-- because the application schema is first in search_path.
CREATE OR REPLACE FUNCTION unaccent(source text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT
AS $function$
    SELECT source
$function$;
