using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class DraftService : IDraftService
    {
        private readonly AppDbContext _db;

        public DraftService(AppDbContext db)
        {
            _db = db;
        }

        public async Task<ModeDraft> CreateDraftAsync(PvPMatch match)
        {
            var allModes = Enum.GetValues<GameMode>().ToList();

            var firstTurn = Random.Shared.Next(0, 2) == 0
                ? match.Player1Id
                : match.Player2Id;

            var draft = new ModeDraft
            {
                Id = Guid.NewGuid(),
                PvPMatchId = match.Id,
                AvailableModes = allModes,
                BannedModes = new List<GameMode>(),
                Step = 0,
                CurrentTurnUserId = firstTurn
            };

            match.Status = PvPMatchStatus.Drafting;
            match.Draft = draft;

            _db.ModeDrafts.Add(draft);
            await _db.SaveChangesAsync();

            return draft;
        }

        public async Task<ModeDraft> GetDraftAsync(Guid matchId)
        {
            return await _db.ModeDrafts
                .Include(d => d.PvPMatch)
                .FirstAsync(d => d.PvPMatchId == matchId);
        }

        public async Task<ModeDraft> BanModeAsync(Guid matchId, Guid userId, GameMode bannedMode)
        {
            var draft = await _db.ModeDrafts
                .Include(d => d.PvPMatch)
                .FirstAsync(d => d.PvPMatchId == matchId);

            if (draft.PvPMatch.Status != PvPMatchStatus.Drafting)
                throw new Exception("Draft is not active");

            if (draft.CurrentTurnUserId != userId)
                throw new Exception("Not your turn");

            if (!draft.AvailableModes.Contains(bannedMode))
                throw new Exception("Mode is already banned or invalid");

            draft.AvailableModes = draft.AvailableModes
                .Where(m => m != bannedMode)
                .ToList();
            draft.BannedModes = draft.BannedModes
                .Append(bannedMode)
                .ToList();
            draft.Step++;

            if (draft.AvailableModes.Count == 1)
            {
                var finalMode = draft.AvailableModes.First();
                draft.PvPMatch.SelectedMode = finalMode;
                draft.PvPMatch.Status = PvPMatchStatus.Ready;
            }
            else
            {
                draft.CurrentTurnUserId =
                    draft.CurrentTurnUserId == draft.PvPMatch.Player1Id
                    ? draft.PvPMatch.Player2Id
                    : draft.PvPMatch.Player1Id;
            }

            await _db.SaveChangesAsync();
            return draft;
        }
    }
}
