namespace GeoQuiz_backend.Domain.Entities
{
    public class Subscription
    {
        public Guid Id { get; set; }

        public Guid UserId { get; set; }
        public User User { get; set; } = null!;

        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }

        public string Type { get; set; } = null!;
        public bool IsActive { get; set; }
    }
}
