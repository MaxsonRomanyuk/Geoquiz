using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using MongoDB.Driver;

namespace GeoQuiz_backend.Infrastructure.Mongo
{
    public class QuestionRepository : IQuestionRepository
    {
        private readonly IMongoCollection<Question> _collection;

        public QuestionRepository(MongoContext context)
        {
            _collection = context.GetCollection<Question>("questions");
        }

        public async Task<List<Question>> GetAllAsync()
            => await _collection.Find(_ => true).ToListAsync();

        public async Task<List<Question>> GetByTypeAsync(GameMode mode)
            => await _collection.Find(q => q.Type == mode).ToListAsync();

        public async Task AddManyAsync(IEnumerable<Question> questions)
            => await _collection.InsertManyAsync(questions);
        public async Task<List<Question>> GetByIdsAsync(List<string> ids)
        {
            return await _collection.Find(q => ids.Contains(q.Id)).ToListAsync();
        }
        public async Task<Question> GetByIdAsync(string id)
        {
            return await _collection
                .Find(q => q.Id == id)
                .FirstOrDefaultAsync()
                ?? throw new Exception($"Question {id} not found");
        }
    }
}
