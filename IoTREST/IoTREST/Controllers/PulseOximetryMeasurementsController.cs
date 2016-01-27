using System;
using System.Data.Entity.Infrastructure;
using System.Linq;
using System.Net;
using System.Web.Http;
using System.Web.Http.OData;
using IoTREST.Models;

namespace IoTREST.Controllers
{
    public class PulseOximetryMeasurementsController : ODataController
    {
        private OximetryContext db = new OximetryContext();

        // GET: odata/PulseOximetryMeasurements
        [EnableQuery]
        public IQueryable<PulseOximetryMeasurement> GetPulseOximetryMeasurements()
        {
            return db.PulseOximetryMeasurements;
        }

        // GET: odata/PulseOximetryMeasurements(5)
        [EnableQuery]
        public SingleResult<PulseOximetryMeasurement> GetPulseOximetryMeasurement([FromODataUri] int key)
        {
            return SingleResult.Create(db.PulseOximetryMeasurements.Where(pulseOximetryMeasurement => pulseOximetryMeasurement.Id == key));
        }

        // PUT: odata/PulseOximetryMeasurements(5)
        public IHttpActionResult Put([FromODataUri] int key, Delta<PulseOximetryMeasurement> patch)
        {
            Validate(patch.GetEntity());

            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            PulseOximetryMeasurement pulseOximetryMeasurement = db.PulseOximetryMeasurements.Find(key);
            if (pulseOximetryMeasurement == null)
            {
                return NotFound();
            }

            patch.Put(pulseOximetryMeasurement);

            try
            {
                db.SaveChanges();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!PulseOximetryMeasurementExists(key))
                {
                    return NotFound();
                }
                else
                {
                    throw;
                }
            }

            return Updated(pulseOximetryMeasurement);
        }

        // POST: odata/PulseOximetryMeasurements
        public IHttpActionResult Post(PulseOximetryMeasurement pulseOximetryMeasurement)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            const double TOLERANCE = 0.1;

            if (
                db.PulseOximetryMeasurements.Count(measurement => Math.Abs(measurement.HeartRate - pulseOximetryMeasurement.HeartRate) < TOLERANCE &&
                                                                  Math.Abs(measurement.BloodOxygenSaturation - pulseOximetryMeasurement.BloodOxygenSaturation) < TOLERANCE &&
                                                                  measurement.HeartRateUnit == pulseOximetryMeasurement.HeartRateUnit &&
                                                                  measurement.BloodOxygenSaturationUnit == pulseOximetryMeasurement.BloodOxygenSaturationUnit &&
                                                                  measurement.PatientIdentification == pulseOximetryMeasurement.PatientIdentification &&
                                                                  measurement.TimeStamp == pulseOximetryMeasurement.TimeStamp) == 0)
            {
                db.PulseOximetryMeasurements.Add(pulseOximetryMeasurement);
                db.SaveChanges();
            }

            return Created(pulseOximetryMeasurement);
        }

        // PATCH: odata/PulseOximetryMeasurements(5)
        [AcceptVerbs("PATCH", "MERGE")]
        public IHttpActionResult Patch([FromODataUri] int key, Delta<PulseOximetryMeasurement> patch)
        {
            Validate(patch.GetEntity());

            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            PulseOximetryMeasurement pulseOximetryMeasurement = db.PulseOximetryMeasurements.Find(key);
            if (pulseOximetryMeasurement == null)
            {
                return NotFound();
            }

            patch.Patch(pulseOximetryMeasurement);

            try
            {
                db.SaveChanges();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!PulseOximetryMeasurementExists(key))
                {
                    return NotFound();
                }
                else
                {
                    throw;
                }
            }

            return Updated(pulseOximetryMeasurement);
        }

        // DELETE: odata/PulseOximetryMeasurements(5)
        public IHttpActionResult Delete([FromODataUri] int key)
        {
            PulseOximetryMeasurement pulseOximetryMeasurement = db.PulseOximetryMeasurements.Find(key);
            if (pulseOximetryMeasurement == null)
            {
                return NotFound();
            }

            db.PulseOximetryMeasurements.Remove(pulseOximetryMeasurement);
            db.SaveChanges();

            return StatusCode(HttpStatusCode.NoContent);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                db.Dispose();
            }
            base.Dispose(disposing);
        }

        private bool PulseOximetryMeasurementExists(int key)
        {
            return db.PulseOximetryMeasurements.Count(e => e.Id == key) > 0;
        }
    }
}
