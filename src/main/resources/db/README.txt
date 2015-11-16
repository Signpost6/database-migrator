Place desired sql file in this folder ('/db') and the callback will execute the relevant callback.

(refer http://flywaydb.org/documentation/callbacks.html)

beforeMigrate.sql       -	Before Migrate runs
beforeEachMigrate.sql   -	Before every single migration during Migrate
afterEachMigrate.sql    -	After every single migration during Migrate
afterMigrate.sql        -	After Migrate runs
beforeClean.sql         -	Before Clean runs
afterClean.sql          -	After Clean runs
beforeInfo.sql          -	Before Info runs
afterInfo.sql           -	After Info runs
beforeValidate.sql      -	Before Validate runs
afterValidate.sql       -	After Validate runs
beforeBaseline.sql      -	Before Baseline runs
afterBaseline.sql       -	After Baseline runs
beforeRepair.sql        -	Before Repair runs
afterRepair.sql         -	After Repair runs


Additional to the Flyway Callbacks.  The DbMigrator also supports 2 additional tasks.

provision.sql           -   Executed when provisioning the database environment.  Create users, tablespaces etc.  Requires admin user/password
destroy.sql             -   Executed when destroying the database environment.  Remove schemas, tablespaces etc.  Requires admin user/password