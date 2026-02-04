using MongoDB.Driver;
namespace GeoQuiz_backend.Infrastructure.Mongo
{
    public class MongoContext
    {
        private readonly IMongoDatabase _database;

        public MongoContext(IConfiguration config)
        {
            var client = new MongoClient(config["MongoDb:ConnectionString"]);
            _database = client.GetDatabase(config["MongoDb:Database"]);
        }

        public IMongoCollection<T> GetCollection<T>(string name)
        {
            return _database.GetCollection<T>(name);
        }
    }
}
