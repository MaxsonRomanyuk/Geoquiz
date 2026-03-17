namespace GeoQuiz_backend.Application.Payloads.Bots
{
    public class BotAnswer
    {
        public Guid PlayerId { get; set; }
        public int SelectedIndex { get; set; }
        public int ResponseTimeMs { get; set; }
        public bool IsCorrect { get; set; }

    }
}
