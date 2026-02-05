using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace GeoQuiz_backend.Controllers.PvP
{
    public class PvPController : Controller
    {
        // GET: PvPController
        public ActionResult Index()
        {
            return View();
        }

        // GET: PvPController/Details/5
        public ActionResult Details(int id)
        {
            return View();
        }

        // GET: PvPController/Create
        public ActionResult Create()
        {
            return View();
        }

        // POST: PvPController/Create
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Create(IFormCollection collection)
        {
            try
            {
                return RedirectToAction(nameof(Index));
            }
            catch
            {
                return View();
            }
        }

        // GET: PvPController/Edit/5
        public ActionResult Edit(int id)
        {
            return View();
        }

        // POST: PvPController/Edit/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Edit(int id, IFormCollection collection)
        {
            try
            {
                return RedirectToAction(nameof(Index));
            }
            catch
            {
                return View();
            }
        }

        // GET: PvPController/Delete/5
        public ActionResult Delete(int id)
        {
            return View();
        }

        // POST: PvPController/Delete/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Delete(int id, IFormCollection collection)
        {
            try
            {
                return RedirectToAction(nameof(Index));
            }
            catch
            {
                return View();
            }
        }
    }
}
