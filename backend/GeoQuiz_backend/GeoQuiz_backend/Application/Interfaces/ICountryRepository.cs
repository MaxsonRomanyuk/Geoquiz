using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface ICountryRepository
    {
        Task<List<Country>> GetAllAsync();
        Task<List<Country>> GetAllInRegionAsync(Region region);
        Task<Country?> GetByIdAsync(string id);
        Task<List<Country>> GetByIdsAsync(List<string> ids);
        Task CreateAsync(Country country);
    }
}
