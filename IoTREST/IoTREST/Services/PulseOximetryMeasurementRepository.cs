using System.Linq;
using IoTREST.Models;

namespace IoTREST.Services
{
    public class PulseOximetryMeasurementRepository
    {
        public PulseOximetryMeasurement[] GetAllMeasurements()
        {
            using (var db = new OximetryContext())
            {
                var query = from m in db.PulseOximetryMeasurements
                    orderby m.TimeStamp
                    select m;

                return query.ToArray();
            }
        }

        public void SavePulseOximetryMeasurement(PulseOximetryMeasurement measurement)
        {
            using (var db = new OximetryContext())
            {
                db.PulseOximetryMeasurements.Add(measurement);
                db.SaveChanges();
            }
        }
    }
}