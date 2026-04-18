using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.Mongo;
using MongoDB.Driver;

namespace GeoQuiz_backend.Infrastructure.Persistence.Mongo.Repositories
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

        public async Task<List<Country>> GetByIdsAsync(List<string> ids)
        {
            var countries = await _countries
                .Find(q => ids.Contains(q.Id))
                .ToListAsync();

            return ids.Select(id => countries.First(c => c.Id == id)).ToList();
        }

        public async Task CreateAsync(Country country)
        {
            await _countries.InsertOneAsync(country);
        }
    }
}
