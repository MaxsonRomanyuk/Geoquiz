using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace GeoQuiz_backend.Controllers
{
    public class ContentController : Controller
    {
        // GET: ContentController
        public ActionResult Index()
        {
            return View();
        }

        // GET: ContentController/Details/5
        public ActionResult Details(int id)
        {
            return View();
        }

        // GET: ContentController/Create
        public ActionResult Create()
        {
            return View();
        }

        // POST: ContentController/Create
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

        // GET: ContentController/Edit/5
        public ActionResult Edit(int id)
        {
            return View();
        }

        // POST: ContentController/Edit/5
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

        // GET: ContentController/Delete/5
        public ActionResult Delete(int id)
        {
            return View();
        }

        // POST: ContentController/Delete/5
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
