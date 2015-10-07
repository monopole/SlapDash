package com.example.jregan.slapdash;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * An app.Service to coordinate discovery of presentations by syncslides.
 *
 * General idea: people have an instance of the syncslides app on their device.
 * When a user Alice starts her instance of syncslides, the list of presos
 * (preso == abbreviation of presentation) she sees is the set of 'idle' presos
 * stored in her device-local syncbase instance, plus any 'live' presos offered
 * by others. Alice can offer, or join, exactly one preso at a time.
 *
 * An instance of this class provides the means to see adverts for those 'live'
 * presos, or to let Alice start a new one and let others see an advert for it.
 * * To accomplish this, an instance of this app.Service - which lives on while
 * syncslide's main activity is off the screen - does two primary things.
 *
 * First:
 *
 * In onStartCommand, it starts a local V23 'particpant' service (managed by
 * this instance of android.app.Service) and mounts it in a V23 mounttable (aka
 * MT, aka 'namespace root') under the V23 object name "{someRootPath}/{name}".
 *
 * Alice's name on her device inspires the value of {name}, and {someRootPath}
 * will be user input or simply "syncslides".  The name of the MT itself is
 * either hardcoded or discovered by some means (e.g. by asking a web service
 * for it).
 *
 * This 'participant' service has one method: Hello(string)
 *
 * If a non-nil, non-empty string argument is passed, Alice can interpret it as
 * the name of another participant service, that she can establish a client for
 * and call Hello in return.
 *
 * Regardless of the argument, Hello() returns either a struct summarizing a
 * _live_ presentation offered by Alice (it's title, a cover slide thumbnail,
 * etc.), or it returns nil, indicating Alice is not offering a live
 * presentation at the moment.  Alice can offer exactly one live preso or none.
 *
 *
 * Second:
 *
 * In onStartCommand, this instance globs the MT looking for names that look
 * like participants, opens clients to them, and calls Hello on them, both to
 * introduce Alice, and to find out what presentations are live. This scan must
 * be performed at least once to discover other participants, but it could be
 * done periodically to keep an internal participant table tidy and recover from
 * race conditions (two participants join at the same time and miss each other
 * in the first scan).
 *
 *
 * The participant service is only for discovery, and will not be used to
 * coordinate the live preso, e.g. it won't get people on the same slide, it
 * won't allow them to ask questions, etc.  That's a job for syncbase.
 *
 * Alternatives:
 *
 * NO_CACHED_PARTICIPANT_TABLE:  On startup, mount a service as described above,
 * and glob the mount table for other services only to see their names (to help
 * pick a unique name for Alice), and ask them if they are live.  No data is
 * sent (no argument to 'Hello()').  When any preso goes live, the presenter
 * freshly scans the MT for all particpants, and calls Hello({name}) them to
 * tell them about the new live preso. They change their UX accordingly. Upside:
 * with no cache, the code is less complex.  Downside: if the MT becomes
 * unavailable, discovery breaks.
 *
 * MT_LIVE_ONLY:  The MT only gets entries for participants currently conducting
 * live _presos_, and everyone is obligated to poll it periodically to see the
 * list of live presos. Upside: the participant scanning the MT doesn't need to
 * make an RPC to find out if a preso is live.  Downside: When a preso goes
 * live, there's no way to immediately inform other participants - said
 * participants won't see the new live preso until they scan the MT, and if the
 * MT becomes unavailable, discovery breaks.
 *
 * BLE: To replace or supplement the above with a BLE style Advertise/Scan, this
 * app.service could create a BluetoothLeAdvertiser to advertise a live preso,
 * and it could create a BluetoothLeScanner to scan for live presos.  The data
 * exchanged should be enough to start coordinating via syncbase, without the
 * need for a Vanadium particpant service and a MT to put them in.  But it would
 * only work at BLE range.  Correctly done, both methods could exist at the same
 * time, since the end result is a list of live presos one can join.
 *
 * MDNS: Same discussion as BLE, though "BLE range" becomes local wifi network.
 */
public class PresoMtDiscoveryService extends Service {
    private static final String LOGTAG = "PresoMtDiscoveryService";

    public PresoMtDiscoveryService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
