using GeoQuiz.Backend.Domain.Mongo;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface ICountryRepository
{
    Task<List<Country>> GetAllAsync();
    Task<Country?> GetByIdAsync(string id);
    Task CreateAsync(Country country);
}
