using GeoQuiz_backend.Application.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace GeoQuiz_backend.API.Controllers.SoloGame
{
    [ApiController]
    [Route("api/content")]
    public class ContentController : ControllerBase
    {
        private readonly ICountryRepository _countryRepo;
        public ContentController(ICountryRepository countryRepo)
        {
            _countryRepo = countryRepo;
        }
        [HttpGet("bootstrap")]
        public async Task<IActionResult> Bootstrap()
        {
            var countries = await _countryRepo.GetAllAsync();
            return Ok(new 
            {
                Countries  = countries,
            });
        }
    }
}
