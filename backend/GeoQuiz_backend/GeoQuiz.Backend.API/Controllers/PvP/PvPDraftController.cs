using GeoQuiz.Backend.Application.DTOs.PvP;
using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Domain.Enums;
using GeoQuiz.Backend.DTOs.PvP;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz.Backend.API.Controllers.PvP;

[ApiController]
[Route("api/pvp/draft")]
[Authorize]
public class PvPDraftController : ControllerBase
{
    private readonly IDraftService _draftService;
    private readonly IQuestionSetService _questionSetService;

    public PvPDraftController(
        IDraftService draftService,
        IQuestionSetService questionSetService)
    {
        _draftService = draftService;
        _questionSetService = questionSetService;
    }

    [HttpGet("{matchId}")]
    public async Task<IActionResult> GetDraft(Guid matchId)
    {
        var draft = await _draftService.GetDraftAsync(matchId);

        var dto = new ModeDraftDto
        {
            MatchId = draft.PvPMatchId,
            CurrentTurnUserId = draft.CurrentTurnUserId,
            AvailableModes = draft.AvailableModes,
            BannedModes = draft.BannedModes,
            Step = draft.Step,
            Status = draft.PvPMatch.Status,
            SelectedMode = draft.PvPMatch.SelectedMode
        };

        return Ok(dto);
    }

    [HttpPost("{matchId}/ban")]
    public async Task<IActionResult> BanMode(
        Guid matchId,
        [FromQuery] GameMode mode,
        [FromQuery] AppLanguage lang)
    {
        var userId = Guid.Parse(
            User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
        );

        var draft = await _draftService.BanModeAsync(matchId, userId, mode);

        if (draft.PvPMatch.Status == PvPMatchStatus.Ready)
        {
            await _questionSetService.CreateForMatchAsync(matchId, lang);
        }

        var dto = new ModeDraftDto
        {
            MatchId = draft.PvPMatchId,
            CurrentTurnUserId = draft.CurrentTurnUserId,
            AvailableModes = draft.AvailableModes,
            BannedModes = draft.BannedModes,
            Step = draft.Step,
            Status = draft.PvPMatch.Status,
            SelectedMode = draft.PvPMatch.SelectedMode
        };

        return Ok(dto);
    }
}