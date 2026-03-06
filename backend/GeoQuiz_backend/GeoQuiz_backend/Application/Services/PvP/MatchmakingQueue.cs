namespace GeoQuiz_backend.Application.Services.PvP
{
    public class MatchmakingQueue
    {
        private readonly Queue<Guid> _queue = new();

        public Guid? Enqueue(Guid userId)
        {
            if (_queue.Count == 0)
            {
                _queue.Enqueue(userId);
                return null;
            }

            var opponentId = _queue.Dequeue();
            return opponentId;
        }
        public void Remove(Guid userId)
        {
            var remaining = _queue.Where(id => id != userId).ToList();
            _queue.Clear();

            foreach (var id in remaining)
            {
                _queue.Enqueue(id);
            }
        }

        public bool Contains(Guid userId)
        {
            return _queue.Contains(userId);
        }

        public int Count => _queue.Count;
    }
}
