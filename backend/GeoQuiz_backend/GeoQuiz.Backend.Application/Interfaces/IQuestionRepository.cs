using GeoQuiz.Backend.Domain.Enums;
using GeoQuiz.Backend.Domain.Mongo;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IQuestionRepository
{
    Task<List<Question>> GetAllAsync();
    Task<List<Question>> GetByTypeAsync(GameMode mode);
    Task AddManyAsync(IEnumerable<Question> questions);
    Task<List<Question>> GetByIdsAsync(List<string> ids);
    Task<Question> GetByIdAsync(string id);
}
