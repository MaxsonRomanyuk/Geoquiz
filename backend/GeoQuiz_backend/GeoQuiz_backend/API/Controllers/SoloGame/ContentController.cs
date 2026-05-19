using GeoQuiz_backend.Application.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace GeoQuiz_backend.API.Controllers.SoloGame
{
    [ApiController]
    [Route("api/content")]
    public class ContentController : ControllerBase
    {
        private readonly ICountryRepository _countryRepository;
        public ContentController(ICountryRepository countryRepo)
        {
            _countryRepository = countryRepo;
        }
        [HttpGet("bootstrap")]
        public async Task<IActionResult> Bootstrap()
        {
            var countries = await _countryRepository.GetAllAsync();
            return Ok(new 
            {
                Countries  = countries,
            });
        }
    }
}
