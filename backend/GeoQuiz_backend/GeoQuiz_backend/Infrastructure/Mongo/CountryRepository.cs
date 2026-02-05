using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Mongo;
using MongoDB.Driver;

namespace GeoQuiz_backend.Infrastructure.Mongo
{
    public class CountryRepository : ICountryRepository
    {
        private readonly IMongoCollection<Country> _countries;

        public CountryRepository(MongoContext context)
        {
            _countries = context.GetCollection<Country>("countries");
        }

        public async Task<List<Country>> GetAllAsync()
        {
            return await _countries.Find(_ => true).ToListAsync();
        }

        public async Task<Country?> GetByIdAsync(string id)
        {
            return await _countries.Find(c => c.Id == id).FirstOrDefaultAsync();
        }

        public async Task CreateAsync(Country country)
        {
            await _countries.InsertOneAsync(country);
        }
    }
}
