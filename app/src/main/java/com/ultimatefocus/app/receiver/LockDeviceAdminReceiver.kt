/**
 * Empty DeviceAdminReceiver that exists only so the launcher can hold the
 * `force-lock` device-admin policy, required by DevicePolicyManager.lockNow().
 *
 * No callbacks are overridden — default no-op behaviour is fine. The receiver
 * is paired with res/xml/device_admin.xml which declares the single policy
 * needed for ULT-18 (screen lock).
 *
 * Covers: ULT-18
 */
package com.ultimatefocus.app.receiver

import android.app.admin.DeviceAdminReceiver

class LockDeviceAdminReceiver : DeviceAdminReceiver()
