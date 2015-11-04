# postgresql-async-play & mysql-async-play -- sample application
This is a simple sample application for both the PostgreSQL and MySQL portions
of the db-async-play drivers, as such its use of the two datastores is a little
contrived, sacrificing referential integrity guarantees for the sake of
incorporating both drivers.

This Play application can be found running on
[Heroku](https://db-async-play-sample.herokuapp.com/).

## Design Notes
The sample application relegates its database interaction to an object per data
model.  Those objects in turn are injected the MySQL and PostgreSQL Connection
Pools.

