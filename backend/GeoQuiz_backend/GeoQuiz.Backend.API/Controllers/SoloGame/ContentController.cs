using GeoQuiz.Backend.Application.Interfaces;

using Microsoft.AspNetCore.Mvc;

namespace GeoQuiz.Backend.API.Controllers.SoloGame;

[ApiController]
[Route("api/content")]
public class ContentController : ControllerBase
{
    private readonly IQuestionRepository _questionRepo;
    private readonly ICountryRepository _countryRepo;

    public ContentController(
        IQuestionRepository questionRepo,
        ICountryRepository countryRepo)
    {
        _questionRepo = questionRepo;
        _countryRepo = countryRepo;
    }

    [HttpGet("bootstrap")]
    public async Task<IActionResult> Bootstrap()
    {
        var countries = await _countryRepo.GetAllAsync();
        var questions = await _questionRepo.GetAllAsync();

        return Ok(new
        {
            Countries = countries,
            Questions = questions
        });
    }
}