using GeoQuiz.Backend.Application.DTOs.PvP;
using GeoQuiz.Backend.Application.Interfaces;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz.Backend.API.Controllers.PvP;

[ApiController]
[Route("api/pvp/game")]
[Authorize]
public class PvPGameController : ControllerBase
{
    private readonly IPvPGameSessionService _service;

    public PvPGameController(IPvPGameSessionService service)
    {
        _service = service;
    }

    [HttpPost("{matchId}/start")]
    public async Task<IActionResult> Start(Guid matchId)
    {
        await _service.StartMatchAsync(matchId);
        return Ok();
    }

    [HttpGet("{matchId}/state")]
    public async Task<IActionResult> State(Guid matchId)
    {
        var userId = Guid.Parse(
            User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
        );

        var state = await _service.GetGameStateAsync(matchId, userId);

        return Ok(state);
    }

    [HttpPost("{matchId}/answer")]
    public async Task<IActionResult> Answer(
        Guid matchId,
        [FromBody] SubmitAnswerRequest req)
    {
        var userId = Guid.Parse(
            User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
        );

        var result = await _service.SubmitAnswerAsync(matchId, userId, req);

        return Ok(result);
    }

    [HttpPost("{matchId}/finish")]
    public async Task<IActionResult> Finish(Guid matchId)
    {
        var userId = Guid.Parse(
            User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
        );

        await _service.FinishAsync(matchId, userId);

        return Ok();
    }
}