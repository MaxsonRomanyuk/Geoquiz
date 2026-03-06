using GeoQuiz.Backend.Application.Services.PvP;

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz.Backend.API.Controllers.PvP;

[Authorize]
[ApiController]
[Route("api/pvp")]
public class PvPController : ControllerBase
{
    private readonly MatchmakingService _matchmaking;

    public PvPController(MatchmakingService matchmaking)
    {
        _matchmaking = matchmaking;
    }

    [HttpPost("join")]
    public async Task<IActionResult> JoinQueue()
    {
        var userId = Guid.Parse(
            User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
        );

        var match = await _matchmaking.JoinQueueAsync(userId);

        if (match == null)
            return Ok(new { status = "waiting" });

        return Ok(new
        {
            status = "matched",
            matchId = match.Id,
            opponent = match.Player1Id == userId
                ? match.Player2Id
                : match.Player1Id
        });
    }
}