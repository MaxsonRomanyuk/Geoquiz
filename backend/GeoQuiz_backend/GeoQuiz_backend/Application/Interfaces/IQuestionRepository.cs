using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IQuestionRepository
    {
        Task<List<Question>> GetAllAsync();
        Task<List<Question>> GetByTypeAsync(GameMode mode);
        Task AddManyAsync(IEnumerable<Question> questions);
        Task<List<Question>> GetByIdsAsync(List<string> ids);
        Task<Question> GetByIdAsync(string id);
    }
}
